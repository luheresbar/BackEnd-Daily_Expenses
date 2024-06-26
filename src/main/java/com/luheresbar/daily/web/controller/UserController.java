package com.luheresbar.daily.web.controller;

import com.luheresbar.daily.domain.User;
import com.luheresbar.daily.domain.dto.MessageDto;
import com.luheresbar.daily.domain.dto.UpdatePasswordDto;
import com.luheresbar.daily.domain.dto.UpdateUserDto;
import com.luheresbar.daily.domain.dto.UserProfileDto;
import com.luheresbar.daily.domain.service.UserService;
import com.luheresbar.daily.persistence.projections.IUserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private Integer currentUser;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @ModelAttribute
    public void extractUserFromToken() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        String userToken = (String) authentication.getPrincipal();
        this.currentUser = Integer.valueOf(userToken);
    }

    // Listar los usuarios registrados solamente con su userId y su fecha de registro
    @GetMapping
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<IUserSummary>> viewUsersSummary() {
        return new ResponseEntity<>(userService.viewUsersSummary(), HttpStatus.OK);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> countUsers() {
        return ResponseEntity.ok(this.userService.countUsers());
    }

    // Como usuario puedo visualizar mi informacion personal registrada en la app, para que pueda saber si debo actualizarla
    @GetMapping("/user")
    public ResponseEntity<Optional<UserProfileDto>> viewInformation() {
        Optional<User> userDB = userService.getById(this.currentUser);
        return userDB.map(user -> ResponseEntity.ok(Optional.of(new UserProfileDto(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRegisterDate(),
                        user.getRoles()
                ))
        )).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/update")
    public ResponseEntity<UserProfileDto> update(@RequestBody UpdateUserDto updateUserDto) {
        User user = new User();
        user.setUserId(this.currentUser);
        user.setUsername(updateUserDto.username());
        user.setEmail(updateUserDto.email());
        user.setPassword(updateUserDto.password());

        Optional<User> userInDb = this.userService.getById(this.currentUser);
        if (userInDb.isPresent()) {
            user.setRoles(userInDb.get().getRoles());
            user.setRegisterDate(userInDb.get().getRegisterDate());
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                user.setUsername(userInDb.get().getUsername());
            }
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                user.setEmail(userInDb.get().getEmail());
            }
            user.setPassword(userInDb.get().getPassword());

            if (userInDb.get().equals(user)) {
                return ResponseEntity.ok(new UserProfileDto(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRegisterDate(),
                        user.getRoles()
                ));
            }

            if (!userInDb.get().getEmail().equals(updateUserDto.email()) && this.passwordEncoder.matches(updateUserDto.password(), userInDb.get().getPassword())) {
                user.setEmail(updateUserDto.email());
            } else if (!userInDb.get().getEmail().equals(updateUserDto.email()) && !this.passwordEncoder.matches(updateUserDto.password(), userInDb.get().getPassword())) {
                return ResponseEntity.badRequest().build();
            }
            User updatedUser = this.userService.save(user);

            return ResponseEntity.ok(new UserProfileDto(
                    updatedUser.getUserId(),
                    updatedUser.getUsername(),
                    updatedUser.getEmail(),
                    updatedUser.getRegisterDate(),
                    updatedUser.getRoles()
            ));
        }
        return ResponseEntity.badRequest().build();
    }

    @Transactional
    @PutMapping("/update-password")
    public ResponseEntity<MessageDto> updatePassword(@RequestBody UpdatePasswordDto dto) { //TODO (Complementar respuesta, ejm, cuando la new password sea igual a la contraseña existente, notificarlo, o que se pueada guardar un registro de contraseñas, para no poner una contraseña que ya hubiere estado en el registro)
        Optional<User> userInDb = this.userService.getById(this.currentUser);
        if (userInDb.isPresent() && !dto.currentPassword().equals(dto.newPassword()) && this.passwordEncoder.matches(dto.currentPassword(), userInDb.get().getPassword())) {

            String passwordEncoded = this.passwordEncoder.encode(dto.newPassword());

            if (this.userService.changePassword(this.currentUser, passwordEncoded)) {
                return ResponseEntity.ok(new MessageDto(true));
            } else {
                return ResponseEntity.ok((new MessageDto(false)));
            }
        }
        return ResponseEntity.ok((new MessageDto(false)));
    }


    //  Unicamente un usuario puede eliminar su propia cuenta.
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser() {
        if (this.userService.delete(this.currentUser)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
