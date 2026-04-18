package com.recipes.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
    private String id;
    private String title;
    private String cuisine1;
    private String cuisine2;
    private String difficulty;
}
