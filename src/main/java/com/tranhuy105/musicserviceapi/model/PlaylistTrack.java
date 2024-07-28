package com.tranhuy105.musicserviceapi.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistTrack {
    private Integer position;
    private LocalDate addedAt;
    private Long addedBy;
    private TrackDetail track;
}
