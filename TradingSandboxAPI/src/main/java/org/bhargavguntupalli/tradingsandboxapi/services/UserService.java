package org.bhargavguntupalli.tradingsandboxapi.services;

import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;

import java.math.BigDecimal;

public interface UserService {
    UserDto registerNewUser(UserDto dto);
    UserDto getUserDtoByUsername(String username);
    UserDto updateProfile(String username, UserDto dto);
    UserDto creditBalance(String username, BigDecimal amount);
    UserDto debitBalance(String username, BigDecimal amount);
    UserDto toggleTheme(String username);
}
