package com.cuttypaws.service.interf;

import com.cuttypaws.dto.LoginHistoryDto;
import com.cuttypaws.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface LoginHistoryService {

    void saveSuccess(User user, HttpServletRequest request);

    void saveFailed(String email, String failureReason, HttpServletRequest request);

    List<LoginHistoryDto> getMyLoginHistory();
}