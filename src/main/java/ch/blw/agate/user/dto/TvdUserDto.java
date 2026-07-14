package ch.blw.agate.user.dto;

import lombok.Builder;

@Builder
public record TvdUserDto(String loginId, String firstName, String name, String extId, String clientName) {
}
