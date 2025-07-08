package org.bhargavguntupalli.tradingsandboxapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
// ignore any JSON props you’re not binding in this call
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private Long id;

    // required on registration
    @NotBlank(message="username is required", groups = { Register.class, UpdateProfile.class })
    private String username;

    @NotBlank(message="password is required", groups = Register.class)
    private String password;

    // required when updating email
    @NotBlank(message="email is required", groups = UpdateProfile.class)
    @Email(message="must be a valid email", groups = UpdateProfile.class)
    private String email;

    @NotBlank(message="firstName is required", groups = UpdateProfile.class)
    private String firstName;

    @NotBlank(message="lastName is required", groups = UpdateProfile.class)
    private String lastName;

    private String themePreference;

    // required when crediting/debiting balance
    @NotNull(message="amount is required", groups = ChangeBalance.class)
    @DecimalMin(value="0.01", message="amount must be positive", groups = ChangeBalance.class)
    private BigDecimal amount;

    // read‐only on most calls
    private BigDecimal cashBalance;

    /** Validation groups so we only enforce certain fields per endpoint */
    public interface Register {}
    public interface UpdateProfile {}
    public interface ChangeBalance {}
}
