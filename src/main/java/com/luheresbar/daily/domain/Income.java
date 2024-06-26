package com.luheresbar.daily.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Income {

    private Integer incomeId;
    private Double income;
    private String description;
    private String incomeDate;
    private Integer userId;
    private String accountName;
    private String categoryName;

}
