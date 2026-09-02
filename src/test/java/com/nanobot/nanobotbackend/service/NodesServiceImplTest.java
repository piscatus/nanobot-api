package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.DepositRecordEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.repository.DepositRecordsRepository;
import com.nanobot.nanobotbackend.service.chain.NanoAddressIndex;
import com.nanobot.nanobotbackend.service.chain.NanoWebSocketService;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class NodesServiceImplTest {

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private MessagesService messagesService;

  @Mock
  private QueuesService queuesService;

  @Mock
  private TransactionsService transactionsService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private DepositRecordsRepository depositRecordsRepository;

  @Mock
  private NanoWebSocketService nanoWebSocketService;

  /**
   * Real rather than mocked: CoreServices exposes its members as public final
   * fields, which a mock leaves null.
   */
  private CoreServices coreServices;

  private NanoAddressIndex nanoAddressIndex;
  private NodesServiceImpl nodesService;
  private MockWebServer mockWebServer;

  @BeforeEach
  void setUp() throws Exception {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    nanoAddressIndex = new NanoAddressIndex();
    nodesService = newService(HttpClient.newHttpClient(), null);
  }

  private NodesServiceImpl newService(HttpClient client, String workServerUrl) {
    return new NodesServiceImpl(
      currenciesService,
      messagesService,
      queuesService,
      coreServices,
      transactionsService,
      depositRecordsRepository,
      nanoAddressIndex,
      nanoWebSocketService,
      client,
      workServerUrl
    );
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockWebServer != null) {
      mockWebServer.shutdown();
    }
  }

  @Test
  void getCharsBeforeUnderscoreShouldReturnCorrectIndex() {
    assertEquals(0, NodesServiceImpl.getCharsBeforeUnderscore("nano"));
    assertEquals(4, NodesServiceImpl.getCharsBeforeUnderscore("nano_123"));
    assertEquals(0, NodesServiceImpl.getCharsBeforeUnderscore(""));
    assertEquals(2, NodesServiceImpl.getCharsBeforeUnderscore("ab_cd"));
  }

  @Test
  void getWorkBodyShouldReturnValidJson() throws JSONException {
    JSONObject result = nodesService.getWorkBody("hash123", "ffffffc000000000");

    assertNotNull(result);
    assertEquals("work_generate", result.getString("action"));
    assertEquals("hash123", result.getString("hash"));
    assertEquals("ffffffc000000000", result.getString("difficulty"));
  }

  @Test
  void hexStringToByteArrayShouldConvertCorrectly() {
    byte[] result = NodesServiceImpl.hexStringToByteArray("0a0b0c");
    assertNotNull(result);
    assertEquals(3, result.length);
    assertEquals(0x0a, result[0] & 0xff);
    assertEquals(0x0b, result[1] & 0xff);
    assertEquals(0x0c, result[2] & 0xff);
  }

  @Test
  void bytesToHexShouldConvertCorrectly() {
    byte[] bytes = new byte[] { 10, 11, 12 };
    String result = NodesServiceImpl.bytesToHex(bytes);
    assertEquals("0a0b0c", result);
  }

  @Test
  void hexStringToByteArrayAndBytesToHexShouldRoundTrip() {
    String original = "deadbeef";
    byte[] bytes = NodesServiceImpl.hexStringToByteArray(original);
    String roundTrip = NodesServiceImpl.bytesToHex(bytes);
    assertEquals(original, roundTrip);
  }

  @Test
  void derivePrivateKeyShouldProduce32Bytes() {
    byte[] seed = new byte[32];
    Arrays.fill(seed, (byte) 1);
    byte[] result = NodesServiceImpl.derivePrivateKey(seed, 0);
    assertNotNull(result);
    assertEquals(32, result.length);
  }

  @Test
  void derivePrivateKeyShouldProduceDifferentOutputForDifferentIndex() {
    byte[] seed = new byte[32];
    Arrays.fill(seed, (byte) 1);
    byte[] key0 = NodesServiceImpl.derivePrivateKey(seed, 0);
    byte[] key1 = NodesServiceImpl.derivePrivateKey(seed, 1);
    assertFalse(Arrays.equals(key0, key1));
  }

  @Test
  void processBlockCountShouldReturnTrueWhenInLineWithPeers()
    throws JSONException {
    JSONObject response = blockCount(222_510_100L, 222_471_100L);
    JSONObject telemetry = peerTelemetry(
      new long[] { 222_510_090L, 222_510_120L, 222_510_110L },
      new long[] { 222_471_090L, 222_471_120L, 222_471_110L }
    );

    assertTrue(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldReturnTrueWhenLocalUnconfirmedGapMatchesNetwork()
    throws JSONException {
    // ~39k unconfirmed locally, same as peers — not actually behind
    JSONObject response = blockCount(222_510_100L, 222_471_100L);
    JSONObject telemetry = peerTelemetry(
      new long[] {
        222_510_080L,
        222_510_100L,
        222_510_120L,
        222_510_095L,
        222_510_105L
      },
      new long[] {
        222_471_080L,
        222_471_100L,
        222_471_120L,
        222_471_095L,
        222_471_105L
      }
    );

    assertTrue(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldReturnFalseWhenCementedLagsPeers()
    throws JSONException {
    JSONObject response = blockCount(222_510_100L, 222_470_000L);
    JSONObject telemetry = peerTelemetry(
      new long[] { 222_510_100L, 222_510_110L, 222_510_120L },
      new long[] { 222_471_100L, 222_471_110L, 222_471_120L }
    );

    assertFalse(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldReturnFalseWhenBlockCountLagsPeers()
    throws JSONException {
    JSONObject response = blockCount(222_509_000L, 222_471_100L);
    JSONObject telemetry = peerTelemetry(
      new long[] { 222_510_100L, 222_510_110L, 222_510_120L },
      new long[] { 222_471_100L, 222_471_110L, 222_471_120L }
    );

    assertFalse(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldReturnFalseWhenTelemetryMissing()
    throws JSONException {
    JSONObject response = blockCount(100L, 100L);

    assertFalse(nodesService.processBlockCount(response, null));
    assertFalse(nodesService.processBlockCount(response, new JSONObject()));
  }

  @Test
  void processBlockCountShouldReturnFalseWhenTooFewPeers() throws JSONException {
    JSONObject response = blockCount(100L, 100L);
    JSONObject telemetry = peerTelemetry(
      new long[] { 100L, 100L },
      new long[] { 100L, 100L }
    );

    assertFalse(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldReturnFalseWhenInvalidJson() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("invalid", "data");
    JSONObject telemetry = peerTelemetry(
      new long[] { 100L, 100L, 100L },
      new long[] { 100L, 100L, 100L }
    );

    assertFalse(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldIgnoreBrokenPeerSamples() throws JSONException {
    JSONObject response = blockCount(100L, 100L);
    JSONArray metrics = new JSONArray();
    metrics.put(new JSONObject().put("block_count", 1).put("cemented_count", 1));
    metrics.put(
      new JSONObject().put("block_count", 100).put("cemented_count", 100)
    );
    metrics.put(
      new JSONObject().put("block_count", 100).put("cemented_count", 100)
    );
    metrics.put(
      new JSONObject().put("block_count", 101).put("cemented_count", 100)
    );
    metrics.put(new JSONObject().put("block_count", 0).put("cemented_count", 0));
    JSONObject telemetry = new JSONObject().put("metrics", metrics);

    assertTrue(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void processBlockCountShouldReturnTrueWhenAllBlocksCemented()
    throws JSONException {
    JSONObject response = blockCount(100L, 100L);
    JSONObject telemetry = peerTelemetry(
      new long[] { 100L, 100L, 101L },
      new long[] { 100L, 100L, 100L }
    );

    assertTrue(nodesService.processBlockCount(response, telemetry));
  }

  @Test
  void sendRequestToNodeShouldAcceptBlockCountWhenTelemetryShowsNodeInSync()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
      new MockResponse()
        .setBody("{\"count\":\"100\",\"cemented\":\"100\",\"unchecked\":\"0\"}")
    );
    mockWebServer.enqueue(
      new MockResponse()
        .setBody(
          peerTelemetry(
            new long[] { 100L, 100L, 100L },
            new long[] { 100L, 100L, 100L }
          ).toString()
        )
    );

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);

    NodesServiceImpl serviceWithMock = newService(
      HttpClient.newHttpClient(),
      baseUrl
    );

    boolean result = serviceWithMock.sendRequestToNode(
      null,
      null,
      null,
      currency,
      "block_count",
      new JSONObject().put("action", "block_count")
    );

    assertTrue(result);
    assertEquals(2, mockWebServer.getRequestCount());
  }

  private static JSONObject blockCount(long count, long cemented)
    throws JSONException {
    return new JSONObject()
      .put("unchecked", 0)
      .put("count", count)
      .put("cemented", cemented);
  }

  private static JSONObject peerTelemetry(long[] counts, long[] cemented)
    throws JSONException {
    JSONArray metrics = new JSONArray();
    for (int i = 0; i < counts.length; i++) {
      metrics.put(
        new JSONObject()
          .put("block_count", counts[i])
          .put("cemented_count", cemented[i])
      );
    }
    return new JSONObject().put("metrics", metrics);
  }

  @Test
  void addressToPublicKeyShouldThrowForInvalidChar() {
    CurrencyEntity currency = new CurrencyEntity();
    assertThrows(
      IllegalArgumentException.class,
      () -> NodesServiceImpl.addressToPublicKey(currency, "ban_1invalid@char")
    );
  }

  @Test
  void addressToPublicKeyShouldReturn32BytesForValidBananoAddress() {
    CurrencyEntity currency = new CurrencyEntity();
    // Valid Banano-style address: ban_ + base32(32 bytes) + 8 char checksum
    // Minimal valid format: ban_ + 52 chars base32 + 8 checksum = 64 chars after prefix
    String validAddress = "ban_1111111111111111111111111111111111111111111111111111111111111111";
    byte[] result = NodesServiceImpl.addressToPublicKey(currency, validAddress);
    assertNotNull(result);
    assertEquals(32, result.length);
  }

  @Test
  void hexStringToByteArrayShouldHandleEmptyString() {
    byte[] result = NodesServiceImpl.hexStringToByteArray("");
    assertNotNull(result);
    assertEquals(0, result.length);
  }

  @Test
  void getWorkBodyShouldIncludeAllRequiredFields() throws JSONException {
    JSONObject result = nodesService.getWorkBody("abc123", "fffffe0000000000");

    assertNotNull(result);
    assertEquals("work_generate", result.getString("action"));
    assertEquals("abc123", result.getString("hash"));
    assertEquals("fffffe0000000000", result.getString("difficulty"));
    assertEquals(3, result.length());
  }

  @Test
  void processSendShouldUpdateQueueWhenNodeAndWorkServerRespondSuccessfully()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = mockWebServer.url("/").toString();

    // 1. account_info response (Nano node format)
    String accountInfoResponse =
      """
      {
        "frontier": "FF84533A571D953A596EA401FD41743AC85D04F406E76FDE4408EAED50B473C5",
        "balance": "1000000000000000000000000000000",
        "representative": "nano_3t6k35gi95xu6tergt6p69ck76ogmitsa8mnijtpxm9fkcm736xtoncuohr3"
      }
      """;
    mockWebServer.enqueue(new MockResponse().setBody(accountInfoResponse));

    // 2. work_generate response (work server)
    String workResponse = "{\"work\": \"2b3d689bbcb21dca\"}";
    mockWebServer.enqueue(new MockResponse().setBody(workResponse));

    // 3. block_create response
    String blockCreateResponse =
      """
      {
        "hash": "E2FB233EF4554077A7BF1AA85851D5BF0B36965D2B0FB504B2BC778AB89917D3",
        "block": {
          "type": "state",
          "account": "nano_3qgmh14nwztqw4wmcdzy4xpqeejey68chx6nciczwn9abji7ihhum9qtpmdr",
          "previous": "FF84533A571D953A596EA401FD41743AC85D04F406E76FDE4408EAED50B473C5",
          "representative": "nano_3t6k35gi95xu6tergt6p69ck76ogmitsa8mnijtpxm9fkcm736xtoncuohr3",
          "balance": "999000000000000000000000000000",
          "link": "19D3D919475DEED4696B5D13018151D1AF88B2BD3BCFF048B45031C1F36D1858",
          "signature": "3BFBA64A775550E6D49DF1EB8EEC2136DCD74F090E2ED658FBD9E80F17CB1C9F",
          "work": "2b3d689bbcb21dca"
        }
      }
      """;
    mockWebServer.enqueue(new MockResponse().setBody(blockCreateResponse));

    // 4. process response
    String processResponse =
      "{\"hash\": \"E2FB233EF4554077A7BF1AA85851D5BF0B36965D2B0FB504B2BC778AB89917D3\"}";
    mockWebServer.enqueue(new MockResponse().setBody(processResponse));

    QueueEntity queue = new QueueEntity();
    queue.setId("q1");
    queue.setSourceAddress("ban_1111111111111111111111111111111111111111111111111111111111111111");
    queue.setTargetAddress("ban_1111111111111111111111111111111111111111111111111111111111111112");
    queue.setRaw("1000000000000000000000000000");
    queue.setTicker("BAN");
    queue.setSeed("0000000000000000000000000000000000000000000000000000000000000001");

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    NodesServiceImpl serviceWithMock = newService(
      HttpClient.newHttpClient(),
      baseUrl
    );
    when(
      queuesService.updateQueueProgress(any(), any(), any(), any(), any())
    ).thenReturn(true);

    serviceWithMock.processSend(queue, currency);

    ArgumentCaptor<String> queueIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<QueueDto> dtoCaptor = ArgumentCaptor.forClass(QueueDto.class);
    verify(queuesService).updateQueue(queueIdCaptor.capture(), dtoCaptor.capture());

    assertEquals("q1", queueIdCaptor.getValue());
    assertTrue(dtoCaptor.getValue().getProcessed());
    assertEquals(
      "E2FB233EF4554077A7BF1AA85851D5BF0B36965D2B0FB504B2BC778AB89917D3",
      dtoCaptor.getValue().getBlockHash()
    );

    // The hash has to reach the database before the block reaches the network,
    // or a crash in between leaves a published send with nothing recording it.
    verify(queuesService).updateQueueProgress(
      eq("q1"),
      eq(null),
      eq(null),
      eq("E2FB233EF4554077A7BF1AA85851D5BF0B36965D2B0FB504B2BC778AB89917D3"),
      eq(null)
    );
  }

  /**
   * The double-send this whole mechanism exists to prevent: a previous pass
   * published the block but died before recording it. Rebuilding would spend
   * from the new frontier and pay the user twice.
   */
  @Test
  void processSendShouldAdoptABlockThatWasAlreadyPublished() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    // block_info finds it, so it went out.
    mockWebServer.enqueue(
      new MockResponse()
        .setBody(
          "{\"block_account\":\"ban_111\",\"amount\":\"1000\"," +
          "\"confirmed\":\"true\",\"contents\":{\"type\":\"state\"}}"
        )
    );

    QueueEntity queue = recoveryQueue();
    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    newService(HttpClient.newHttpClient(), baseUrl).processSend(
      queue,
      currency
    );

    assertTrue(queue.getProcessed());
    verify(queuesService).updateQueueProgress(
      eq("q1"),
      eq(null),
      eq(null),
      eq(null),
      eq(true)
    );
    // Only the block_info lookup; nothing was rebuilt or published.
    assertEquals(1, mockWebServer.getRequestCount());
  }

  /**
   * An unreachable node is not evidence that the block never went out, so the
   * entry has to wait rather than be rebuilt.
   */
  @Test
  void processSendShouldDeferWhenPublicationCannotBeDetermined()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
      new MockResponse().setBody("{\"error\": \"Unable to reach node\"}")
    );

    QueueEntity queue = recoveryQueue();
    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    newService(HttpClient.newHttpClient(), baseUrl).processSend(
      queue,
      currency
    );

    assertFalse(queue.getProcessed());
    verify(queuesService, never()).updateQueue(any(), any());
    assertEquals(1, mockWebServer.getRequestCount());
  }

  /**
   * "Block not found" is the one answer that proves nothing was published, so
   * rebuilding is safe. The frontier is untouched, so the rebuilt block is the
   * one already recorded.
   */
  @Test
  void processSendShouldRebuildWhenTheRecordedBlockNeverWentOut()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
      new MockResponse().setBody("{\"error\": \"Block not found\"}")
    );
    // Then the ordinary build path runs again.
    mockWebServer.enqueue(
      new MockResponse().setBody("{\"error\": \"Account not found\"}")
    );

    QueueEntity queue = recoveryQueue();
    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    newService(HttpClient.newHttpClient(), baseUrl).processSend(
      queue,
      currency
    );

    // Two calls means it moved past recovery into the rebuild.
    assertEquals(2, mockWebServer.getRequestCount());
    verify(queuesService, never()).updateQueue(any(), any());
  }

  /**
   * A node that cannot be reached says nothing about whether the send could
   * ever work. Counting those attempts would let a brief outage cancel a good
   * withdrawal within three seconds.
   */
  @Test
  void processSendShouldNotCountAnAttemptWhenTheNodeIsUnreachable()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    QueueEntity queue = new QueueEntity();
    queue.setId("q1");
    queue.setUserId("user-1");
    queue.setSourceAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111111"
    );
    queue.setTargetAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111112"
    );
    queue.setRaw("1000");
    queue.setTicker("BAN");
    queue.setSeed(
      "0000000000000000000000000000000000000000000000000000000000000001"
    );

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    newService(HttpClient.newHttpClient(), baseUrl).processSend(
      queue,
      currency,
      Map.of()
    );

    verify(queuesService, never()).updateQueueProgress(
      any(),
      any(),
      any(),
      any(),
      any()
    );
    assertNull(queue.getAttempts());
  }

  /**
   * A node that answered and rejected the account is a permanent condition, so
   * it counts toward the refund threshold.
   */
  @Test
  void processSendShouldCountAnAttemptWhenTheNodeRejectsTheAccount()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
      new MockResponse().setBody("{\"error\": \"Account not found\"}")
    );

    QueueEntity queue = new QueueEntity();
    queue.setId("q1");
    queue.setUserId("user-1");
    queue.setSourceAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111111"
    );
    queue.setTargetAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111112"
    );
    queue.setRaw("1000");
    queue.setTicker("BAN");
    queue.setSeed(
      "0000000000000000000000000000000000000000000000000000000000000001"
    );

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    newService(HttpClient.newHttpClient(), baseUrl).processSend(
      queue,
      currency,
      Map.of()
    );

    assertEquals(1, queue.getAttempts());
    verify(queuesService).updateQueueProgress("q1", 1, null, null, null);
  }

  /**
   * A sweep moves the bot's own funds and has no debited balance behind it, so
   * there is nothing to refund and the entry must be left alone.
   */
  @Test
  void exhaustedSweepsShouldNotBeRefunded() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    mockWebServer.enqueue(
      new MockResponse().setBody("{\"error\": \"Account not found\"}")
    );

    QueueEntity queue = new QueueEntity();
    queue.setId("sweep-1");
    queue.setUserId(null);
    queue.setSourceAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111111"
    );
    queue.setTargetAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111112"
    );
    queue.setRaw("1000");
    queue.setTicker("BAN");
    queue.setSeed(
      "0000000000000000000000000000000000000000000000000000000000000001"
    );
    queue.setAttempts(2);

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    newService(HttpClient.newHttpClient(), baseUrl).processSend(
      queue,
      currency,
      Map.of()
    );

    assertEquals(3, queue.getAttempts());
    verify(queuesService, never()).deleteQueue(any());
  }

  private QueueEntity recoveryQueue() {
    QueueEntity queue = new QueueEntity();
    queue.setId("q1");
    queue.setSourceAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111111"
    );
    queue.setTargetAddress(
      "ban_1111111111111111111111111111111111111111111111111111111111111112"
    );
    queue.setRaw("1000000000000000000000000000");
    queue.setTicker("BAN");
    queue.setSeed(
      "0000000000000000000000000000000000000000000000000000000000000001"
    );
    queue.setBlockHash(
      "E2FB233EF4554077A7BF1AA85851D5BF0B36965D2B0FB504B2BC778AB89917D3"
    );
    queue.setProcessed(false);
    return queue;
  }

  @Test
  void processSendShouldReturnEarlyWhenAccountNotFound() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    String accountNotFoundResponse = "{\"error\": \"Account not found\"}";
    mockWebServer.enqueue(new MockResponse().setBody(accountNotFoundResponse));

    QueueEntity queue = new QueueEntity();
    queue.setId("q1");
    queue.setSourceAddress("ban_1111111111111111111111111111111111111111111111111111111111111111");
    queue.setTargetAddress("ban_1111111111111111111111111111111111111111111111111111111111111112");
    queue.setRaw("1000");
    queue.setTicker("BAN");
    queue.setSeed("0000000000000000000000000000000000000000000000000000000000000001");

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setSendDifficulty("ffffffc000000000");

    NodesServiceImpl serviceWithMock = newService(
      HttpClient.newHttpClient(),
      baseUrl
    );

    serviceWithMock.processSend(queue, currency);

    verify(queuesService, never()).updateQueue(any(), any());
  }

  @Test
  void processReceiveShouldUpdateQueueWhenAccountExistsAndReceivesSuccessfully()
    throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    // 1. account_info - existing account
    String accountInfoResponse =
      """
      {
        "frontier": "FF84533A571D953A596EA401FD41743AC85D04F406E76FDE4408EAED50B473C5",
        "balance": "500000000000000000000000000000",
        "representative": "nano_3t6k35gi95xu6tergt6p69ck76ogmitsa8mnijtpxm9fkcm736xtoncuohr3"
      }
      """;
    mockWebServer.enqueue(new MockResponse().setBody(accountInfoResponse));

    // 2. work_generate
    mockWebServer.enqueue(new MockResponse().setBody("{\"work\": \"2b3d689bbcb21dca\"}"));

    // 3. block_create
    String blockCreateResponse =
      """
      {
        "hash": "RECV1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890AB",
        "block": {"type": "state", "work": "2b3d689bbcb21dca"}
      }
      """;
    mockWebServer.enqueue(new MockResponse().setBody(blockCreateResponse));

    // 4. process
    mockWebServer.enqueue(
      new MockResponse()
        .setBody(
          "{\"hash\": \"RECV1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890AB\"}"
        )
    );

    QueueEntity queue = new QueueEntity();
    queue.setId("q2");
    queue.setBlockHash("PENDING_SEND_BLOCK_HASH_1234567890ABCDEF1234567890ABCDEF12");
    queue.setTargetAddress("ban_1111111111111111111111111111111111111111111111111111111111111111");
    queue.setRaw("1000000000000000000000000000");
    queue.setTicker("BAN");
    queue.setSeed("0000000000000000000000000000000000000000000000000000000000000001");

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setReceiveDifficulty("fffffe0000000000");
    currency.setOpenDifficulty("fffffe0000000000");

    NodesServiceImpl serviceWithMock = newService(
      HttpClient.newHttpClient(),
      baseUrl
    );

    serviceWithMock.processReceive(queue, currency);

    ArgumentCaptor<QueueDto> dtoCaptor = ArgumentCaptor.forClass(QueueDto.class);
    verify(queuesService).updateQueue(eq("q2"), dtoCaptor.capture());
    assertTrue(dtoCaptor.getValue().getProcessed());
    assertEquals(
      "RECV1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890AB",
      dtoCaptor.getValue().getBlockHash()
    );
  }

  @Test
  void processReceiveShouldUpdateQueueWhenAccountDoesNotExistYet() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();

    // 1. account_info - Account not found (new account, first receive)
    mockWebServer.enqueue(
      new MockResponse().setBody("{\"error\": \"Account not found\"}")
    );

    // 2. work_generate
    mockWebServer.enqueue(new MockResponse().setBody("{\"work\": \"abcd1234abcd1234\"}"));

    // 3. block_create (open block)
    mockWebServer.enqueue(
      new MockResponse()
        .setBody(
          """
          {
            "hash": "OPEN1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890",
            "block": {"type": "state", "work": "abcd1234abcd1234"}
          }
          """
        )
    );

    // 4. process
    mockWebServer.enqueue(
      new MockResponse()
        .setBody(
          "{\"hash\": \"OPEN1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890\"}"
        )
    );

    QueueEntity queue = new QueueEntity();
    queue.setId("q3");
    queue.setBlockHash("PENDING_SEND_HASH_1234567890ABCDEF1234567890ABCDEF1234567890ABCD");
    queue.setTargetAddress("ban_1111111111111111111111111111111111111111111111111111111111111111");
    queue.setRaw("500000000000000000000000000000");
    queue.setTicker("BAN");
    queue.setSeed("0000000000000000000000000000000000000000000000000000000000000001");

    CurrencyEntity currency = new CurrencyEntity();
    currency.setNodeUrl(baseUrl);
    currency.setReceiveDifficulty("fffffe0000000000");
    currency.setOpenDifficulty("fffffe0000000000");

    NodesServiceImpl serviceWithMock = newService(
      HttpClient.newHttpClient(),
      baseUrl
    );

    serviceWithMock.processReceive(queue, currency);

    verify(queuesService).updateQueue(eq("q3"), any(QueueDto.class));
  }

  // Websocket confirmation routing. Payloads follow the shape documented for
  // the node's confirmation topic, since that is the contract being relied on.

  private static final String BOT_USER_ID = "test-bot-user-id";
  private static final String BOT_SEED =
    "1111111111111111111111111111111111111111111111111111111111111111";
  private static final String USER_SEED =
    "2222222222222222222222222222222222222222222222222222222222222222";
  private static final String SEND_HASH =
    "0E889F83E28152A70E87B92D846CA3D8966F3AEEC65E11B25F7B4E6760C57CA3";
  private static final String RECEIVE_HASH =
    "4E9003ABD469D1F58A70518234016797FA654B494A2627B8583052629A91689E";
  private static final String SENDER =
    "nano_1tgkjkq9r96zd3pkr7edj8e4qbu3wr3ps6ettzse8hmoa37nurua7faupjhc";
  private static final String STRANGER =
    "nano_3dmtrrws3pocycmbqwawk6xs7446qxa36fcncush4s1pejk16ksbmakis32c";

  private static UserDetailsEntity user(String userId, String seed) {
    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setUserId(userId);
    entity.setSeed(seed);
    return entity;
  }

  private static CurrencyEntity nanoCurrency() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setId("xno-id");
    currency.setTicker("XNO");
    return currency;
  }

  private static JSONObject confirmation(
    String hash,
    String amount,
    String blockAccount,
    String subtype,
    String destination,
    String balance
  ) throws JSONException {
    JSONObject block = new JSONObject()
      .put("type", "state")
      .put("account", blockAccount)
      .put("balance", balance)
      .put("subtype", subtype);
    if (destination != null) {
      block.put("link_as_account", destination);
    }
    return new JSONObject()
      .put("account", blockAccount)
      .put("amount", amount)
      .put("hash", hash)
      .put("confirmation_type", "active_quorum")
      .put("block", block);
  }

  @Test
  void handleConfirmationShouldQueueDepositWhenSendReachesKnownAddress()
    throws JSONException {
    UserDetailsEntity bot = user(BOT_USER_ID, BOT_SEED);
    UserDetailsEntity depositor = user("user-1", USER_SEED);
    String depositAddress = CryptoUtil.deriveAddress(depositor, "XNO");
    nanoAddressIndex.add("XNO", depositAddress, "user-1");

    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(nanoCurrency()));
    when(userDetailsService.getUserDetailsByUserId(BOT_USER_ID))
      .thenReturn(Optional.of(bot));
    when(userDetailsService.getUserDetailsByUserId("user-1"))
      .thenReturn(Optional.of(depositor));
    when(queuesService.isDepositQueued("XNO", SEND_HASH)).thenReturn(false);
    when(
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        "XNO",
        SEND_HASH,
        0L
      )
    )
      .thenReturn(false);

    nodesService.handleConfirmation(
      "XNO",
      confirmation(SEND_HASH, "5", SENDER, "send", depositAddress, "0")
    );

    ArgumentCaptor<QueueDto> captor = ArgumentCaptor.forClass(QueueDto.class);
    verify(queuesService).createQueue(captor.capture());

    QueueDto queued = captor.getValue();
    assertEquals(LevelDto.RECEIVE, queued.getLevel());
    assertEquals("user-1", queued.getUserId());
    assertEquals(SENDER, queued.getSourceAddress());
    assertEquals(depositAddress, queued.getTargetAddress());
    assertEquals(SEND_HASH, queued.getBlockHash());
    assertEquals(SEND_HASH, queued.getSourceHash());
    assertEquals("5", queued.getRaw());
    assertFalse(queued.getProcessed());
  }

  @Test
  void handleConfirmationShouldIgnoreSendToAnAddressTheBotDoesNotHold()
    throws JSONException {
    UserDetailsEntity bot = user(BOT_USER_ID, BOT_SEED);

    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(nanoCurrency()));
    when(userDetailsService.getUserDetailsByUserId(BOT_USER_ID))
      .thenReturn(Optional.of(bot));

    nodesService.handleConfirmation(
      "XNO",
      confirmation(SEND_HASH, "5", SENDER, "send", STRANGER, "0")
    );

    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void handleConfirmationShouldNotQueueADepositThatIsAlreadyQueued()
    throws JSONException {
    UserDetailsEntity bot = user(BOT_USER_ID, BOT_SEED);
    UserDetailsEntity depositor = user("user-1", USER_SEED);
    String depositAddress = CryptoUtil.deriveAddress(depositor, "XNO");
    nanoAddressIndex.add("XNO", depositAddress, "user-1");

    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(nanoCurrency()));
    when(userDetailsService.getUserDetailsByUserId(BOT_USER_ID))
      .thenReturn(Optional.of(bot));
    when(userDetailsService.getUserDetailsByUserId("user-1"))
      .thenReturn(Optional.of(depositor));
    when(queuesService.isDepositQueued("XNO", SEND_HASH)).thenReturn(true);

    nodesService.handleConfirmation(
      "XNO",
      confirmation(SEND_HASH, "5", SENDER, "send", depositAddress, "0")
    );

    verify(queuesService, never()).createQueue(any());
  }

  /**
   * The node repeats confirmations, so a deposit that has already been credited
   * must not be pocketed a second time.
   */
  @Test
  void handleConfirmationShouldNotQueueADepositThatWasAlreadyCredited()
    throws JSONException {
    UserDetailsEntity bot = user(BOT_USER_ID, BOT_SEED);
    UserDetailsEntity depositor = user("user-1", USER_SEED);
    String depositAddress = CryptoUtil.deriveAddress(depositor, "XNO");
    nanoAddressIndex.add("XNO", depositAddress, "user-1");

    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(nanoCurrency()));
    when(userDetailsService.getUserDetailsByUserId(BOT_USER_ID))
      .thenReturn(Optional.of(bot));
    when(userDetailsService.getUserDetailsByUserId("user-1"))
      .thenReturn(Optional.of(depositor));
    when(queuesService.isDepositQueued("XNO", SEND_HASH)).thenReturn(false);
    when(
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        "XNO",
        SEND_HASH,
        0L
      )
    )
      .thenReturn(true);

    nodesService.handleConfirmation(
      "XNO",
      confirmation(SEND_HASH, "5", SENDER, "send", depositAddress, "0")
    );

    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void handleConfirmationShouldQueueTheSweepWhenAUserReceiveConfirms()
    throws JSONException {
    UserDetailsEntity bot = user(BOT_USER_ID, BOT_SEED);
    UserDetailsEntity depositor = user("user-1", USER_SEED);
    String botAddress = CryptoUtil.deriveAddress(bot, "XNO");
    String depositAddress = CryptoUtil.deriveAddress(depositor, "XNO");
    nanoAddressIndex.add("XNO", depositAddress, "user-1");

    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(nanoCurrency()));
    when(userDetailsService.getUserDetailsByUserId(BOT_USER_ID))
      .thenReturn(Optional.of(bot));
    when(userDetailsService.getUserDetailsByUserId("user-1"))
      .thenReturn(Optional.of(depositor));
    when(queuesService.getProcessedQueueByBlockHash("XNO", RECEIVE_HASH))
      .thenReturn(Optional.empty());
    when(queuesService.isSweepQueued("XNO", depositAddress))
      .thenReturn(false);

    nodesService.handleConfirmation(
      "XNO",
      confirmation(RECEIVE_HASH, "5", depositAddress, "receive", null, "5")
    );

    ArgumentCaptor<QueueDto> captor = ArgumentCaptor.forClass(QueueDto.class);
    verify(queuesService).createQueue(captor.capture());

    QueueDto swept = captor.getValue();
    assertEquals(LevelDto.SEND, swept.getLevel());
    assertEquals(depositAddress, swept.getSourceAddress());
    assertEquals(botAddress, swept.getTargetAddress());
    assertEquals("5", swept.getRaw());
    assertNull(swept.getUserId());
  }

  @Test
  void handleConfirmationShouldTakeHotWalletLiquidityFromTheBlock()
    throws JSONException {
    UserDetailsEntity bot = user(BOT_USER_ID, BOT_SEED);
    String botAddress = CryptoUtil.deriveAddress(bot, "XNO");
    nanoAddressIndex.add("XNO", botAddress, BOT_USER_ID);

    CurrencyEntity currency = nanoCurrency();
    currency.setLiquidity("1");

    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(currency));
    when(userDetailsService.getUserDetailsByUserId(BOT_USER_ID))
      .thenReturn(Optional.of(bot));
    when(queuesService.getProcessedQueueByBlockHash("XNO", SEND_HASH))
      .thenReturn(Optional.empty());

    nodesService.handleConfirmation(
      "XNO",
      confirmation(SEND_HASH, "5", botAddress, "send", STRANGER, "42")
    );

    verify(currenciesService).updateChainState("xno-id", "42", null, null);
    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void handleConfirmationShouldIgnoreANotificationWithoutBlockContents()
    throws JSONException {
    nodesService.handleConfirmation(
      "XNO",
      new JSONObject().put("hash", SEND_HASH).put("amount", "5")
    );

    verify(currenciesService, never()).getCurrencyByTicker(any());
    verify(queuesService, never()).createQueue(any());
  }

  /**
   * The node returns "" instead of {} for an empty result set, which every
   * batch where nobody has a pending deposit hits.
   */
  @Test
  void processReceivableShouldAcceptTheNodesEmptyResultEncoding()
    throws JSONException {
    JSONObject response = new JSONObject().put("blocks", "");

    assertDoesNotThrow(() ->
      nodesService.processReceivable(
        Map.of(),
        BOT_SEED,
        nanoCurrency(),
        response
      )
    );

    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void processBalancesShouldAcceptTheNodesEmptyResultEncoding()
    throws JSONException {
    JSONObject response = new JSONObject().put("balances", "");

    assertDoesNotThrow(() ->
      nodesService.processBalances(
        Map.of(),
        "nano_1bot",
        nanoCurrency(),
        response
      )
    );

    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void processReceivableShouldSkipAccountsWithNothingPending()
    throws JSONException {
    UserDetailsEntity depositor = user("user-1", USER_SEED);
    String depositAddress = CryptoUtil.deriveAddress(depositor, "XNO");

    // An account with no receivables is an empty string among the populated
    // entries, and must not abandon the rest of the batch.
    JSONObject response = new JSONObject()
      .put("blocks", new JSONObject().put(depositAddress, ""));

    nodesService.processReceivable(
      Map.of(depositAddress, depositor),
      BOT_SEED,
      nanoCurrency(),
      response
    );

    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void confirmReceiveShouldNotCreditWhenTheDepositIsAlreadyRecorded() {
    QueueEntity queue = new QueueEntity();
    queue.setId("q9");
    queue.setLevel(LevelDto.RECEIVE);
    queue.setProcessed(true);
    queue.setUserId("user-1");
    queue.setTicker("XNO");
    queue.setRaw("5");
    queue.setBlockHash(RECEIVE_HASH);
    queue.setSourceHash(SEND_HASH);

    when(queuesService.deleteQueue("q9")).thenReturn(Optional.of(queue));
    when(depositRecordsRepository.insert(any(DepositRecordEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate deposit"));

    nodesService.confirmReceive(queue, nanoCurrency(), Map.of(), true);

    verify(messagesService, never()).createMessage(any());
  }

  @Test
  void confirmReceiveShouldKeyTheGuardOnTheIncomingSendHash() {
    QueueEntity queue = new QueueEntity();
    queue.setId("q10");
    queue.setLevel(LevelDto.RECEIVE);
    queue.setProcessed(true);
    queue.setUserId("user-1");
    queue.setTicker("XNO");
    queue.setRaw("5");
    // processReceive overwrites blockHash with the published receive block, so
    // only sourceHash still identifies the deposit itself.
    queue.setBlockHash(RECEIVE_HASH);
    queue.setSourceHash(SEND_HASH);

    when(queuesService.deleteQueue("q10")).thenReturn(Optional.of(queue));
    when(depositRecordsRepository.insert(any(DepositRecordEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate deposit"));

    nodesService.confirmReceive(queue, nanoCurrency(), Map.of(), true);

    ArgumentCaptor<DepositRecordEntity> captor = ArgumentCaptor.forClass(
      DepositRecordEntity.class
    );
    verify(depositRecordsRepository).insert(captor.capture());

    DepositRecordEntity record = captor.getValue();
    assertEquals(SEND_HASH, record.getTxid());
    assertEquals("XNO", record.getTicker());
    assertEquals("user-1", record.getUserId());
    assertEquals(0L, record.getAddressIndex());
  }
}
