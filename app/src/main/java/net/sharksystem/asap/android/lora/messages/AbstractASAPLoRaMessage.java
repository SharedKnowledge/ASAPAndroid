package net.sharksystem.asap.android.lora.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, property="COMMAND")
@JsonSubTypes( {@JsonSubTypes.Type(DiscoverASAPLoRaMessage.class), @JsonSubTypes.Type(ASAPLoRaMessage.class)})
public abstract class AbstractASAPLoRaMessage {}