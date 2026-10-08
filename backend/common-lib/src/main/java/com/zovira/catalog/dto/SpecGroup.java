package com.zovira.catalog.dto;

import java.util.List;

public record SpecGroup(String group, List<SpecItem> items) {

    public record SpecItem(String name, String value) {
    }
}
