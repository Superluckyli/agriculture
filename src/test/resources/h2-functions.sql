CREATE ALIAS IF NOT EXISTS DATE_FORMAT AS $$
String dateFormat(java.sql.Timestamp timestamp, String pattern) {
    if (timestamp == null) {
        return null;
    }
    java.time.LocalDateTime value = timestamp.toLocalDateTime();
    if (pattern == null || pattern.isBlank()) {
        return value.toString();
    }
    String result = pattern;
    result = result.replace("%Y", String.format("%04d", value.getYear()));
    result = result.replace("%m", String.format("%02d", value.getMonthValue()));
    result = result.replace("%d", String.format("%02d", value.getDayOfMonth()));
    result = result.replace("%H", String.format("%02d", value.getHour()));
    result = result.replace("%i", String.format("%02d", value.getMinute()));
    result = result.replace("%s", String.format("%02d", value.getSecond()));
    result = result.replace("%x", String.format("%04d", value.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)));
    result = result.replace("%v", String.format("%02d", value.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
    return result;
}
$$;
