package ru.github.vacancybot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchTask {

  public static final String ALL_REGIONS = "Все регионы";
  public static final String ANY_KEYWORD = "Любая";
  public static final int NO_SALARY_LIMIT = -1;

  private String id;
  private long ownerId;
  private int regionCode;
  private String regionName;
  private int experienceFrom;
  private int salaryMin;
  private int salaryMax;
  private String keyword;
  private long createdAt;

  @JsonIgnore
  public boolean hasRegion() {
    return regionCode > 0;
  }

  @JsonIgnore
  public boolean hasKeyword() {
    return keyword != null && !keyword.isBlank() && !keyword.equalsIgnoreCase(ANY_KEYWORD);
  }

  @JsonIgnore
  public boolean hasSalaryCeiling() {
    return salaryMax > 0;
  }

  @JsonIgnore
  public boolean isBroadSearch() {
    return !hasRegion() && !hasKeyword();
  }

  public void setRegion(int code, String name) {
    this.regionCode = code;
    this.regionName = name;
  }

  public void setSalaryRange(int min, int max) {
    this.salaryMin = min;
    this.salaryMax = max;
  }

  public void copyFiltersFrom(SearchTask other) {
    setRegion(other.regionCode, other.regionName);
    this.experienceFrom = other.experienceFrom;
    setSalaryRange(other.salaryMin, other.salaryMax);
    this.keyword = other.keyword;
  }
}
