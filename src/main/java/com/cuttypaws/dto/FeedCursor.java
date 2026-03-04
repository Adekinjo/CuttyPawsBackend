package com.cuttypaws.dto;

import java.time.LocalDateTime;

public record FeedCursor(LocalDateTime createdAt, Long id) {}