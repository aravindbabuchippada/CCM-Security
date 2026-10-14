package com.ccm.security.responsepojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsersResponse {

    private Integer id;

    private String loginId;

    private String password;

    private String firstName;

    private String lastName;

    private Integer active;
}