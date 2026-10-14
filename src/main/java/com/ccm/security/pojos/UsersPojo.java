package com.ccm.security.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsersPojo {

    private String loginId;

    private String password;

    private String firstName;

    private String lastName;
}