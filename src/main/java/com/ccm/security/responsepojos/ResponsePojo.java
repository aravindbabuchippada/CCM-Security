package com.ccm.security.responsepojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponsePojo {

    @Builder.Default
    private Integer successFlag = 1;

    @Builder.Default
    private String successMessage = "Data saved successfully !!";

    @Builder.Default
    private String errorMessage = null;
}
