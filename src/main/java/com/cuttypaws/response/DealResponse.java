package com.cuttypaws.response;

import com.cuttypaws.dto.DealDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealResponse {


    private int status;
    private String message;
    private DealDto deal;
    private List<DealDto> dealList;
}
