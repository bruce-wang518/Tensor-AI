package com.example.dto;

import java.util.List;
import java.util.stream.Collectors;

public class DtoConverter {
    public static List<MessageResponse> convertMessages(List<MessageDTO> messageDTOs) {
        return messageDTOs.stream()
                .map(DtoConverter::convertMessage)
                .collect(Collectors.toList());
    }

    public static MessageResponse convertMessage(MessageDTO messageDTO) {
        MessageResponse response = new MessageResponse();
        response.setRole(messageDTO.getRole());
        response.setContent(messageDTO.getContent());
        response.setReference(messageDTO.getReference());
        return response;
    }
}