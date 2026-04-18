package project.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class DailyHealthSummary {
    @Id
    private LocalDate recordDate;

    private String startTime;
    private String endTime;
    private Integer timeInBed;
    private Integer minutesAsleep;
    private Integer minutesAwake;
    private Integer efficiency;

    private Integer deepMins;
    private Integer lightMins;
    private Integer remMins;
    private Integer wakeMins;

    private Double breathingRate;
    private Double skinTempRelative;
}