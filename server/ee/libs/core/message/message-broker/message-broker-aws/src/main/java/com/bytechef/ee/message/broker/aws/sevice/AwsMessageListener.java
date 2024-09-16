package com.bytechef.ee.message.broker.aws.sevice;

public class AwsMessageListener {
//    private final AwsFileStorageService storageService;
//
////    @Autowired
//    public AwsMessageListener(AwsFileStorageService storageService) {
//        this.storageService = storageService;
//    }
//
////    @SqsListener(queueNames = { "${aws.queue}" })
//    public void handle(AwsMessage message) {
//        String key = message.uuid().toString();
//        ByteArrayInputStream is = new ByteArrayInputStream(
//            message.content().getBytes(StandardCharsets.UTF_8)
//        );
//        storageService.upload(key, is);
//    }
}
