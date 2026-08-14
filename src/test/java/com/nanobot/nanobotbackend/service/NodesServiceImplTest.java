package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.Arrays;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodesServiceImplTest {

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private MessagesService messagesService;

  @Mock
  private QueuesService queuesService;

  @Mock
  private CoreServices coreServices;

  @Mock
  private TransactionsService transactionsService;

  private NodesServiceImpl nodesService;
  private MockWebServer mockWebServer;

  @BeforeEach
  void setUp() throws Exception {
    nodesService =
      new NodesServiceImpl(
        currenciesService,
        messagesService,
        queuesService,
        coreServices,
        transactionsService
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
  void processBlockCountShouldReturnTrueWhenNodeHealthy() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("unchecked", 0);
    response.put("cemented", 100);
    response.put("count", 100);

    boolean result = nodesService.processBlockCount(response);

    assertTrue(result);
  }

  @Test
  void processBlockCountShouldReturnTrueWhenSyncing() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("unchecked", 50);
    response.put("cemented", 90);
    response.put("count", 100);

    boolean result = nodesService.processBlockCount(response);

    assertTrue(result);
  }

  @Test
  void processBlockCountShouldReturnFalseWhenTooManyUnchecked() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("unchecked", 150);
    response.put("cemented", 100);
    response.put("count", 250);

    boolean result = nodesService.processBlockCount(response);

    assertFalse(result);
  }

  @Test
  void processBlockCountShouldReturnFalseWhenInvalidJson() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("invalid", "data");

    boolean result = nodesService.processBlockCount(response);

    assertFalse(result);
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
  void processBlockCountShouldReturnTrueWhenAllBlocksCemented() throws JSONException {
    JSONObject response = new JSONObject();
    response.put("unchecked", 0);
    response.put("cemented", 100);
    response.put("count", 100);

    boolean result = nodesService.processBlockCount(response);

    assertTrue(result);
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

    NodesServiceImpl serviceWithMock =
      new NodesServiceImpl(
        currenciesService,
        messagesService,
        queuesService,
        coreServices,
        transactionsService,
        java.net.http.HttpClient.newHttpClient(),
        baseUrl
      );

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

    NodesServiceImpl serviceWithMock =
      new NodesServiceImpl(
        currenciesService,
        messagesService,
        queuesService,
        coreServices,
        transactionsService,
        java.net.http.HttpClient.newHttpClient(),
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

    NodesServiceImpl serviceWithMock =
      new NodesServiceImpl(
        currenciesService,
        messagesService,
        queuesService,
        coreServices,
        transactionsService,
        java.net.http.HttpClient.newHttpClient(),
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

    NodesServiceImpl serviceWithMock =
      new NodesServiceImpl(
        currenciesService,
        messagesService,
        queuesService,
        coreServices,
        transactionsService,
        java.net.http.HttpClient.newHttpClient(),
        baseUrl
      );

    serviceWithMock.processReceive(queue, currency);

    verify(queuesService).updateQueue(eq("q3"), any(QueueDto.class));
  }
}
