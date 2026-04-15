package com.cuttypaws.repository;

import com.cuttypaws.entity.LoginHistory;
import com.cuttypaws.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepo extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findTop20ByUserOrderByCreatedAtDesc(User user);
}