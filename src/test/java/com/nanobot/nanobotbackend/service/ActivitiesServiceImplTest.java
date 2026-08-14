package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.entity.ActivityEntity;
import com.nanobot.nanobotbackend.repository.ActivitiesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DuplicateKeyException;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivitiesServiceImplTest {

    @Mock
    private ActivitiesRepository activitiesRepository;

    @Mock
    private FileLogger fileLogger;

    @InjectMocks
    private ActivitiesServiceImpl activitiesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateActivity() {
        ActivityDto activityDto = new ActivityDto("guild1", "channel1", "user1", new Date());
        ActivityEntity activityEntity = new ActivityEntity(activityDto);
        activityEntity.setId(new ObjectId().toHexString());

        when(activitiesRepository.insert(any(ActivityEntity.class)))
                .thenReturn(activityEntity);

        Optional<ActivityEntity> createdActivity = activitiesService.createActivity(activityDto);

        assertTrue(createdActivity.isPresent());
        assertEquals(activityEntity.getId(), createdActivity.get().getId());
    }

    @Test
    void testCreateActivity_DuplicateKeyException() {
        ActivityDto activityDto = new ActivityDto("guild1", "channel1", "user1", new Date());
        when(activitiesRepository.insert(any(ActivityEntity.class)))
                .thenThrow(new DuplicateKeyException("Duplicate key"));

        Optional<ActivityEntity> createdActivity = activitiesService.createActivity(activityDto);

        assertFalse(createdActivity.isPresent());
    }

    @Test
    void testGetActivities() {
        String id = "12345";
        String guildId = "guild1";
        String channelId = "channel1";
        String userId = "user1";
        Date timestamp = new Date();

        List<ActivityEntity> activities = new ArrayList<>();
        ActivityEntity activity = new ActivityEntity();
        activity.setId(id);
        activity.setGuildId(guildId);
        activity.setChannelId(channelId);
        activity.setUserId(userId);
        activity.setTimestamp(timestamp);
        activities.add(activity);

        when(activitiesRepository.findByGuildIdAndChannelIdAndUserId(guildId, channelId, userId))
                .thenReturn(activities);

        List<ActivityEntity> result = activitiesService.getActivities(guildId, channelId, userId);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetActiveActivities() {
        String guildId = "guild1";
        String channelId = "channel1";
        String userId = "user1";

        RequestDto requestDto = new RequestDto(
            null,
            channelId,
            false,
            null,
            null,
            false,
            guildId,
            null,
            null,
            "",
            2,
            null,
            userId,
            Arrays.asList(userId, "user2"),
            null,
            null
        );
        TransferResponseDto transferResponseDto = 
            new TransferResponseDto();
        GuildConfigurationsDto guildConfigurations =
            new GuildConfigurationsDto();
        guildConfigurations.setMaximumActiveUsers(5);
        guildConfigurations.setMaximumMinutesActive(10);
        transferResponseDto.setGuildConfigurations(guildConfigurations);

        List<ActivityEntity> activitiesList = new ArrayList<>();
        activitiesList.add(new ActivityEntity(
            new ActivityDto(
                guildId,
                channelId,
                "user2",
                new Date()
            )
        ));

        when(activitiesRepository.findByGuildIdAndChannelId(guildId, channelId))
                .thenReturn(activitiesList);

        List<ActivityEntity> result = activitiesService.getActiveActivities(
            requestDto,
            transferResponseDto,
            true
        );

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetActivityById() {
        String id = "12345";
        ActivityEntity activityEntity = new ActivityEntity();
        activityEntity.setId(id);

        when(activitiesRepository.findById(id)).thenReturn(Optional.of(activityEntity));

        Optional<ActivityEntity> result = activitiesService.getActivityById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void testUpdateActivity() {
        String id = "12345";
        ActivityDto activityDto = new ActivityDto("guild1", "channel1", "user1", new Date());
        ActivityEntity existingEntity = new ActivityEntity();
        existingEntity.setId(id);
        existingEntity.setGuildId("guild1");

        when(activitiesRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(activitiesRepository.save(any(ActivityEntity.class)))
                .thenReturn(existingEntity);

        Optional<ActivityEntity> result = activitiesService.updateActivity(id, activityDto);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void testDeleteActivity() {
        String id = "12345";
        ActivityEntity activityEntity = new ActivityEntity();
        activityEntity.setId(id);

        when(activitiesRepository.findById(id)).thenReturn(Optional.of(activityEntity));

        Optional<ActivityEntity> result = activitiesService.deleteActivity(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(activitiesRepository).deleteById(id);
    }

    @Test
    void testUpdateOrCreateActivity_Create() {
        String guildId = "guild1";
        String channelId = "channel1";
        String userId = "user1";

        ActivityEntity activity = new ActivityEntity();
        activity.setGuildId(guildId);
        activity.setChannelId(channelId);
        activity.setUserId(userId);
        activity.setTimestamp(new Date());
        ObjectId id = new ObjectId();
        activity.setId(id.toHexString());

        when(activitiesRepository.findByGuildIdAndChannelIdAndUserId(guildId, channelId, userId))
                .thenReturn(Collections.emptyList());
        when(activitiesRepository.save(any(ActivityEntity.class)))
                .thenReturn(activity);

        ActivityEntity result = activitiesService.updateOrCreateActivity(guildId, channelId, userId);

        assertNotNull(result);
        assertEquals(id.toHexString(), result.getId());
        assertEquals(guildId, result.getGuildId());
    }

    @Test
    void testUpdateOrCreateActivity_Update() {
        String guildId = "guild1";
        String channelId = "channel1";
        String userId = "user1";

        ActivityEntity existingActivity = new ActivityEntity();
        existingActivity.setGuildId(guildId);
        existingActivity.setChannelId(channelId);
        existingActivity.setUserId(userId);
        existingActivity.setId(new ObjectId().toHexString());

        when(activitiesRepository.findByGuildIdAndChannelIdAndUserId(guildId, channelId, userId))
                .thenReturn(Collections.singletonList(existingActivity));
        when(activitiesRepository.save(any(ActivityEntity.class)))
                .thenReturn(existingActivity);

        ActivityEntity result = activitiesService.updateOrCreateActivity(guildId, channelId, userId);

        assertNotNull(result);
        assertEquals(existingActivity.getId(), result.getId());
        assertEquals(guildId, result.getGuildId());
    }
}