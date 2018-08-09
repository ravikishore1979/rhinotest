package com.jsparser.rhino;

public enum JSPageType {
    PAGE("Page"), PREFAB("Prefab"), PARTIAL("Partial");

    String pageType;

    JSPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getPageType() {
        return pageType;
    }
}
