package ex.rr.teamsmessaging.adaptivecard.enums;

public enum Style {

    DEFAULT("default"),
    EMPHASIS("emphasis"),
    ACCENT("accent"),
    GOOD("good"),
    ATTENTION("attention"),
    WARNING("warning");

    public final String name;

    Style(String s) {
        this.name = s;
    }
}
