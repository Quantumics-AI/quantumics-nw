package ai.quantumics.api.enums;

public enum BusinessDay {
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat"),
    SUNDAY("Sun");

    private String day;

    BusinessDay(String day) {
        this.day = day;
    }

    public String getDay() {
        return day;
    }
}
