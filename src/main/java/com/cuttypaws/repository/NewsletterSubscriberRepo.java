package com.cuttypaws.repository;

import com.cuttypaws.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepo extends JpaRepository<NewsletterSubscriber, Long> {

     Optional<NewsletterSubscriber> findByEmail(String email);

     @Modifying
     @Query("UPDATE NewsletterSubscriber ns SET ns.isActive = true WHERE ns.email = :email")
     void resubscribeByEmail(String email);

     @Modifying
     @Query("UPDATE NewsletterSubscriber ns SET ns.isActive = false WHERE ns.email = :email")
     void unsubscribeByEmail(String email);

     List<NewsletterSubscriber> findByIsActiveTrue();

}

