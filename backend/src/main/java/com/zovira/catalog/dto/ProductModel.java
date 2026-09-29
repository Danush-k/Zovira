package com.zovira.catalog.dto;

/** 3D (glTF/GLB) and optional iOS Quick Look (USDZ) assets for the product viewer and AR. */
public record ProductModel(String modelUrl, String arModelUrl, String posterUrl) {
}
