
package ru.github.vacancybot.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegionDirectory {

  public record Region(int code, String name, List<String> searchTokens) {
    public Region(int code, String name) {
      this(code, name, tokenize(name));
    }
  }

  public record RegionPage(List<Region> regions, int page, boolean hasNext) {}

  private static final int REGIONS_PER_PAGE = 3;
  private static final int MIN_SEARCH_LENGTH = 2;

  private static final Pattern CODE_WITH_TEXT = Pattern.compile("^(\\d{1,9})(?:\\s+(.*))?$");
  public static final Pattern NOISE_WORDS = Pattern.compile(
      "\\b(область|республика|край|автономны[йяэ]|округ|город|г)\\b",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
  );
  private static final Pattern CLEAN_PUNCTUATION = Pattern.compile("[—–\\-,.]");

  private static final List<Region> REGIONS = List.of(
      r(1, "Республика Адыгея"),
      r(2, "Республика Башкортостан"),
      r(3, "Республика Бурятия"),
      r(4, "Республика Алтай"),
      r(5, "Республика Дагестан"),
      r(6, "Республика Ингушетия"),
      r(7, "Кабардино-Балкарская Республика"),
      r(8, "Республика Калмыкия"),
      r(9, "Карачаево-Черкесская Республика"),
      r(10, "Республика Карелия"),
      r(11, "Республика Коми"),
      r(12, "Республика Марий Эл"),
      r(13, "Республика Мордовия"),
      r(14, "Республика Саха Якутия"),
      r(15, "Республика Северная Осетия — Алания"),
      r(16, "Республика Татарстан"),
      r(17, "Республика Тыва"),
      r(18, "Удмуртская Республика"),
      r(19, "Республика Хакасия"),
      r(20, "Чеченская Республика"),
      r(21, "Чувашская Республика"),
      r(22, "Алтайский край"),
      r(23, "Краснодарский край"),
      r(24, "Красноярский край"),
      r(25, "Приморский край"),
      r(26, "Ставропольский край"),
      r(27, "Хабаровский край"),
      r(28, "Амурская область"),
      r(29, "Архангельская область"),
      r(30, "Астраханская область"),
      r(31, "Белгородская область"),
      r(32, "Брянская область"),
      r(33, "Владимирская область"),
      r(34, "Волгоградская область"),
      r(35, "Вологодская область"),
      r(36, "Воронежская область"),
      r(37, "Ивановская область"),
      r(38, "Иркутская область"),
      r(39, "Калининградская область"),
      r(40, "Калужская область"),
      r(41, "Камчатский край"),
      r(42, "Кемеровская область"),
      r(43, "Кировская область"),
      r(44, "Костромская область"),
      r(45, "Курганская область"),
      r(46, "Курская область"),
      r(47, "Ленинградская область"),
      r(48, "Липецкая область"),
      r(49, "Магаданская область"),
      r(50, "Московская область"),
      r(51, "Мурманская область"),
      r(52, "Нижегородская область"),
      r(53, "Новгородская область"),
      r(54, "Новосибирская область"),
      r(55, "Омская область"),
      r(56, "Оренбургская область"),
      r(57, "Орловская область"),
      r(58, "Пензенская область"),
      r(59, "Пермский край"),
      r(60, "Псковская область"),
      r(61, "Ростовская область"),
      r(62, "Рязанская область"),
      r(63, "Самарская область"),
      r(64, "Саратовская область"),
      r(65, "Сахалинская область"),
      r(66, "Свердловская область"),
      r(67, "Смоленская область"),
      r(68, "Тамбовская область"),
      r(69, "Тверская область"),
      r(70, "Томская область"),
      r(71, "Тульская область"),
      r(72, "Тюменская область"),
      r(73, "Ульяновская область"),
      r(74, "Челябинская область"),
      r(75, "Забайкальский край"),
      r(76, "Ярославская область"),
      r(77, "Москва"),
      r(78, "Санкт-Петербург"),
      r(79, "Еврейская автономная область"),
      r(83, "Ненецкий автономный округ"),
      r(86, "Ханты-Мансийский автономный округ — Югра"),
      r(87, "Чукотский автономный округ"),
      r(89, "Ямало-Ненецкий автономный округ"),
      r(90, "Запорожская область"),
      r(91, "Республика Крым"),
      r(92, "Севастополь"),
      r(93, "Донецкая Народная Республика"),
      r(94, "Луганская Народная Республика"),
      r(95, "Херсонская область")
  );

  private static final Map<Integer, Region> BY_CODE = REGIONS.stream()
      .collect(Collectors.toUnmodifiableMap(Region::code, Function.identity()));

  public static Optional<Region> findByCode(int code) {
    return Optional.ofNullable(BY_CODE.get(code));
  }

  public static RegionPage page(int requestedPage) {
    int total = REGIONS.size();
    int page = requestedPage;
    int from = (page - 1) * REGIONS_PER_PAGE;
    if (from < 0 || from >= total) {
      from = 0;
      page = 1;
    }
    int to = Math.min(from + REGIONS_PER_PAGE, total);
    return new RegionPage(REGIONS.subList(from, to), page, to < total);
  }

  public static Optional<Region> resolve(String input) {
    if (input == null || input.isBlank()) {
      return Optional.empty();
    }
    Matcher matcher = CODE_WITH_TEXT.matcher(input.trim());
    if (!matcher.matches()) {
      return findBySmartText(input);
    }
    Optional<Region> byCode = findByCode(Integer.parseInt(matcher.group(1)));
    if (byCode.isEmpty()) {
      return Optional.empty();
    }
    String rest = matcher.group(2) == null ? "" : matcher.group(2).trim();
    return rest.isEmpty() ? byCode : findBySmartText(rest).or(() -> byCode);
  }

  public static Optional<Region> findBySmartText(String input) {
    List<String> queryTokens = tokenize(input);
    if (queryTokens.isEmpty()) {
      return Optional.empty();
    }

    boolean hasMinLength = queryTokens.stream().anyMatch(t -> t.length() >= MIN_SEARCH_LENGTH);
    if (!hasMinLength) {
      return Optional.empty();
    }

    return REGIONS.stream()
        .filter(region -> matchesTokensWithTolerance(queryTokens, region))
        .findFirst();
  }

  private static boolean matchesTokensWithTolerance(List<String> queryTokens, Region region) {
    List<String> regionTokens = region.searchTokens();
    int matchedCount = 0;

    for (String qToken : queryTokens) {
      for (String rToken : regionTokens) {
        if (rToken.startsWith(qToken)) {
          matchedCount++;
          break;
        }
      }
    }

    if (matchedCount == 0) {
      return false;
    }

    if (region.code() == 86 || region.code() == 15) {
      return true;
    }

    if (queryTokens.size() == 1) {
      return matchedCount == 1;
    }
    if (queryTokens.size() == 2) {
      return matchedCount >= 1;
    }
    return matchedCount >= (queryTokens.size() - 1);
  }

  private static List<String> tokenize(String text) {
    if (text == null) return List.of();
    String lower = text.toLowerCase(Locale.ROOT);
    String withoutNoise = NOISE_WORDS.matcher(lower).replaceAll("");
    String clean = CLEAN_PUNCTUATION.matcher(withoutNoise).replaceAll(" ");

    return Arrays.stream(clean.split("\\s+"))
        .filter(word -> !word.isBlank())
        .toList();
  }

  public static List<Region> getAll() {
    return REGIONS;
  }

  private static Region r(int code, String name) {
    return new Region(code, name);
  }
}
