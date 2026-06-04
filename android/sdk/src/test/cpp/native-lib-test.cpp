#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "../main/cpp/native-lib.cpp"

class NativeLibTest : public ::testing::Test {
protected:
    void SetUp() override {
        s_backends_loaded = false;
    }
    
    void TearDown() override {
    }
};

TEST_F(NativeLibTest, TestIsValidUtf8_ValidStrings) {
    EXPECT_TRUE(is_valid_utf8(nullptr));
    EXPECT_TRUE(is_valid_utf8(""));
    EXPECT_TRUE(is_valid_utf8("Hello"));
    EXPECT_TRUE(is_valid_utf8("Hello World"));
    EXPECT_TRUE(is_valid_utf8("中文测试"));
    EXPECT_TRUE(is_valid_utf8("こんにちは"));
    EXPECT_TRUE(is_valid_utf8("안녕하세요"));
    EXPECT_TRUE(is_valid_utf8("Hello 世界"));
}

TEST_F(NativeLibTest, TestIsValidUtf8_InvalidStrings) {
    unsigned char invalid1[] = {0xFF, 0xFF, 0x00};
    EXPECT_FALSE(is_valid_utf8(reinterpret_cast<const char*>(invalid1)));
    
    unsigned char invalid2[] = {0xE0, 0x80, 0x80, 0x00};
    EXPECT_TRUE(is_valid_utf8(reinterpret_cast<const char*>(invalid2)));
    
    unsigned char invalid3[] = {0xF8, 0x80, 0x80, 0x80, 0x00};
    EXPECT_FALSE(is_valid_utf8(reinterpret_cast<const char*>(invalid3)));
}

TEST_F(NativeLibTest, TestGetAvailableThreads) {
    int threads = getAvailableThreads();
    EXPECT_GE(threads, 1);
}

TEST_F(NativeLibTest, TestInferenceContextInit) {
    InferenceContext ctx;
    EXPECT_EQ(ctx.model, nullptr);
    EXPECT_EQ(ctx.context, nullptr);
    EXPECT_EQ(ctx.sampler, nullptr);
    EXPECT_EQ(ctx.n_ctx, 2048);
    EXPECT_EQ(ctx.n_threads, 4);
    EXPECT_EQ(ctx.n_past, 0);
    EXPECT_EQ(ctx.n_remaining, 512);
    EXPECT_FALSE(ctx.stop_requested);
    EXPECT_EQ(ctx.callback_obj, nullptr);
    EXPECT_EQ(ctx.callback_method, nullptr);
}

TEST_F(NativeLibTest, TestAndroidLogPrio) {
    EXPECT_EQ(android_log_prio_from_ggml(GGML_LOG_LEVEL_ERROR), ANDROID_LOG_ERROR);
    EXPECT_EQ(android_log_prio_from_ggml(GGML_LOG_LEVEL_WARN), ANDROID_LOG_WARN);
    EXPECT_EQ(android_log_prio_from_ggml(GGML_LOG_LEVEL_INFO), ANDROID_LOG_INFO);
    EXPECT_EQ(android_log_prio_from_ggml(GGML_LOG_LEVEL_DEBUG), ANDROID_LOG_DEBUG);
    EXPECT_EQ(android_log_prio_from_ggml(static_cast<ggml_log_level>(10)), ANDROID_LOG_DEFAULT);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}