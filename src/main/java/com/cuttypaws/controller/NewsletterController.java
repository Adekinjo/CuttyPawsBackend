package com.cuttypaws.controller;

import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.exception.UserAlreadyExistsException;
import com.cuttypaws.repository.ProductRepo;
import com.cuttypaws.response.NewsletterResponse;
import com.cuttypaws.service.interf.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final ProductRepo productRepo;

    @PostMapping("/subscribe")
    public ResponseEntity<NewsletterResponse> subscribe(@RequestParam(required = true) String email) {
        try {
            return ResponseEntity.ok(newsletterService.subscribe(email));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(400).body(
                    NewsletterResponse.builder()
                            .status(400)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    NewsletterResponse.builder()
                            .status(500)
                            .message("An unexpected error occurred: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<NewsletterResponse> unsubscribe(@RequestParam(required = true) String email) {
        try {
            return ResponseEntity.ok(newsletterService.unsubscribe(email));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(
                    NewsletterResponse.builder()
                            .status(404)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    NewsletterResponse.builder()
                            .status(500)
                            .message("An unexpected error occurred: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/subscribers")
    public ResponseEntity<NewsletterResponse> getAllSubscribers() {
        return ResponseEntity.ok(newsletterService.getAllSubscribers());
    }

}
