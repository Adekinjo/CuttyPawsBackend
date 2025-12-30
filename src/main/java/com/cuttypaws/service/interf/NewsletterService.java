package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;
import com.cuttypaws.response.NewsletterResponse;

public interface NewsletterService {
    NewsletterResponse subscribe(String email);
    NewsletterResponse unsubscribe(String email);
    NewsletterResponse getAllSubscribers();
    void sendDailyNewsletter();
}
