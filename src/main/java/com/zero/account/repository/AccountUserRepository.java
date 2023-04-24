package com.zero.account.repository;

import com.zero.account.domain.Account;
import com.zero.account.domain.AccountUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

}
