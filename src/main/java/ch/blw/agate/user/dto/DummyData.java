package ch.blw.agate.user.dto;

import lombok.Builder;

@Builder
public record DummyData(String userId, String role) {
}
