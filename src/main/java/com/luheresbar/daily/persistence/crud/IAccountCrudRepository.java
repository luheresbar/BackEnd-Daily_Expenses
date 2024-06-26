package com.luheresbar.daily.persistence.crud;

import com.luheresbar.daily.persistence.entity.AccountEntity;
import com.luheresbar.daily.persistence.entity.AccountPK;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IAccountCrudRepository extends CrudRepository<AccountEntity, AccountPK> {

    List<AccountEntity> findAllByUserIdOrderByAccountName(Integer userId);

    // Query JPQL
    @Query("SELECT a FROM AccountEntity a WHERE a.available = true AND a.userId = :userId ORDER BY a.accountName")
    List<AccountEntity> findAllAvailableByUserIdOrderByAccountName(@Param("userId") Integer userId);

    // Query JPQL
    @Query("SELECT a FROM AccountEntity a WHERE a.available = false AND a.userId = :userId ORDER BY a.accountName")
    List<AccountEntity> findAllNoAvailableByUserIdOrderByAccountName(@Param("userId") Integer userId);

    @Query(value = "SELECT SUM(available_money) FROM accounts WHERE user_id = :userId AND available = true", nativeQuery = true)
    Double availableMoney(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE AccountEntity a " +
            "SET a.accountName = :newAccountName " +
            "WHERE a.accountName = :accountName AND a.userId = :currentUser ")
    void updateAccountName(@Param("accountName") String accountName, @Param("newAccountName") String newAccountName, @Param("currentUser") Integer currentUser);

    // Query Nativo
//    @Query(value =  "UPDATE users " +
//            "SET user_id = :#{#updateUserId.newUserId} " +
//            "WHERE user_id = :#{#updateUserId.currentUserId}", nativeQuery = true)
//    @Modifying
//    void updateUserId(@Param("updateUserId") UpdateUserIdDto updateUserIdDto);

}
