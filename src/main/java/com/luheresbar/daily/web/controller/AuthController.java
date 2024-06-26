package com.luheresbar.daily.web.controller;

import com.luheresbar.daily.domain.*;
import com.luheresbar.daily.domain.dto.*;
import com.luheresbar.daily.domain.service.*;
import com.luheresbar.daily.web.config.JwtUtil;
import com.luheresbar.daily.web.mailManager.MailManager;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;
  private final UserRoleService userRoleService;
  private final ExpenseCategoryService expenseCategoryService;
  private final IncomeCategoryService incomeCategoryService;
  private final AccountService accountService;
  private final PasswordEncoder passwordEncoder;
  private final MailManager mailManager;
  private final JwtUtil jwtUtil;

  public AuthController(
          AuthenticationManager authenticationManager,
          UserService userService, JwtUtil jwtUtil,
          UserRoleService userRoleService,
          ExpenseCategoryService expenseCategoryService, IncomeCategoryService incomeCategoryService,
          AccountService accountService,
          PasswordEncoder passwordEncoder,
          MailManager mailManager) {
    this.authenticationManager = authenticationManager;
    this.userService = userService;
    this.jwtUtil = jwtUtil;
    this.userRoleService = userRoleService;
    this.expenseCategoryService = expenseCategoryService;
    this.incomeCategoryService = incomeCategoryService;
    this.accountService = accountService;
    this.passwordEncoder = passwordEncoder;
    this.mailManager = mailManager;
  }

  @PostMapping("/login")
  public ResponseEntity<TokenDto> login(@RequestBody LoginDto loginDto) {
    if (loginDto.getEmail() != null && this.userService.existsByEmail(loginDto.getEmail())) {
      Optional<User> userDb = this.userService.findUserByEmail(loginDto.getEmail());
      int userId = userDb.get().getUserId();
      UsernamePasswordAuthenticationToken login = new UsernamePasswordAuthenticationToken(userId, loginDto.getPassword());
      this.authenticationManager.authenticate(login);

      String access_token = this.jwtUtil.createJwt(userId);
      String refresh_token = this.jwtUtil.createJwtRefresh(userId);
      return ResponseEntity.ok(new TokenDto(access_token, refresh_token));
    }
    return ResponseEntity.badRequest().build();
  }

  @PostMapping("/register")
  @Transactional
  public ResponseEntity<Optional<UserProfileDto>> register(@RequestBody User user) {
    if (user.getEmail() == null || !this.userService.existsByEmail(user.getEmail())) {

      String currentDate = String.valueOf(LocalDateTime.now());
      user.setRegisterDate(currentDate);
      String encodedPassword = this.passwordEncoder.encode(user.getPassword());
      user.setPassword(encodedPassword);
      this.userService.save(user);

      Optional<User> userDB = this.userService.findUserByEmail(user.getEmail());

      UserRole userRole = new UserRole();
      userRole.setUserId(userDB.get().getUserId());
      userRole.setRole("USER");
      userRole.setGrantedDate(currentDate);
      this.userRoleService.save(userRole);

      ExpenseCategory expenseCategory = new ExpenseCategory();
      expenseCategory.setUserId(userDB.get().getUserId());
      expenseCategory.setCategoryName("Others");
      this.expenseCategoryService.save(expenseCategory);

      IncomeCategory incomeCategory = new IncomeCategory();
      incomeCategory.setUserId(userDB.get().getUserId());
      incomeCategory.setCategoryName("Others");
      this.incomeCategoryService.save(incomeCategory);

      Account cashAccount = new Account();
      cashAccount.setUserId(userDB.get().getUserId());
      cashAccount.setAccountName("Cash");
      cashAccount.setAvailableMoney(0.0);
      cashAccount.setAvailable(true);
      this.accountService.save(cashAccount);

      Account bankAccount = new Account();
      bankAccount.setUserId(userDB.get().getUserId());
      bankAccount.setAccountName("Bank");
      bankAccount.setAvailableMoney(0.0);
      bankAccount.setAvailable(true);
      this.accountService.save(bankAccount);

      return ResponseEntity.ok(Optional.of(new UserProfileDto(
                      userDB.get().getUserId(),
                      userDB.get().getUsername(),
                      userDB.get().getEmail(),
                      userDB.get().getRegisterDate(),
                      userDB.get().getRoles()

              ))
      );
    }
    return ResponseEntity.badRequest().build(); //TODO (Es necesario hace configurar la respuesta para que no se regrese la contraseña en la respuesta)

  }

  @PostMapping("/is-available")
  public ResponseEntity<ResponseIsAvailableDto> isAvailable(@RequestBody RequestEmailDto email) {
    if (!this.userService.existsByEmail(email.email())) {
      return ResponseEntity.ok(new ResponseIsAvailableDto(true));
    } else {
      return ResponseEntity.ok(new ResponseIsAvailableDto(false));
    }
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<TokenDto> refreshToken(@RequestBody RefreshTokenDto refreshTokenDto) {
    int userId = Integer.parseInt(this.jwtUtil.getUsername(refreshTokenDto.refresh_token()));
    String access_token = this.jwtUtil.createJwt(userId);
    String refresh_token = this.jwtUtil.createJwtRefresh(userId);
    return ResponseEntity.ok(new TokenDto(access_token, refresh_token));
  }


  @PostMapping("/recovery")
  public ResponseEntity<RecoveryResponseDto> recovery(@RequestBody RequestEmailDto email) throws MessagingException {
    if (this.userService.existsByEmail(email.email())) {
      Optional<User> userDb = this.userService.findUserByEmail(email.email());
      int userId = userDb.get().getUserId();
      String token = this.jwtUtil.createJwtRecovery(userId);
      String link = "http://localhost:4200/auth/recovery?token=" + token;

      // Envío del correo electrónico
      this.mailManager.sendEmail(email.email(), "Password Recovery", link);

      return ResponseEntity.ok(new RecoveryResponseDto(link, token)); // TODO (Quitar envio de token en la respuesta del metodo)
    }
    return ResponseEntity.badRequest().build();
  }

  @Transactional
  @PutMapping("/change-password")
  public ResponseEntity<MessageDto> changePassword(@RequestBody ChangePasswordDto dto) { //TODO (Complementar respuesta, ejm, cuando la new password sea igual a la contraseña existente, notificarlo, o que se pueada guardar un registro de contraseñas, para no poner una contraseña que ya hubiere estado en el registro)
    Integer userId = Integer.valueOf(this.jwtUtil.getUsername(dto.token()));
    String passwordEncoded = this.passwordEncoder.encode(dto.newPassword());

    if (this.userService.exists(userId) && this.jwtUtil.isValid(dto.token())) {
      if (this.userService.changePassword(userId, passwordEncoded)) {
        return ResponseEntity.ok(new MessageDto(true));
      } else {
        return ResponseEntity.ok((new MessageDto(false)));
      }
    } else {
      return ResponseEntity.ok((new MessageDto(false)));
    }
  }


}