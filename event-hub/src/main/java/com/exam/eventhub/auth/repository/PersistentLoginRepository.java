package com.exam.eventhub.auth.repository;

import com.exam.eventhub.auth.model.PersistentLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersistentLoginRepository extends JpaRepository <PersistentLogin, String>{
}
