#import "ReactNativeLocalDownload.h"
#import <UIKit/UIKit.h>

@implementation ReactNativeLocalDownload
RCT_EXPORT_MODULE()

- (void)localDownload:(NSString *)uri
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  dispatch_async(dispatch_get_main_queue(), ^{
    NSURL *fileURL = [NSURL fileURLWithPath:uri];
    if (![fileURL isFileURL] || ![[NSFileManager defaultManager] fileExistsAtPath:uri]) {
      reject(@"invalid_file", @"The file URI is not valid or the file does not exist", nil);
      return;
    }

    UIActivityViewController *controller = [[UIActivityViewController alloc]
        initWithActivityItems:@[fileURL]
        applicationActivities:nil];

    UIViewController *rootVC = UIApplication.sharedApplication.delegate.window.rootViewController;
    [rootVC presentViewController:controller animated:YES completion:nil];

    resolve(nil);
  });
}

#pragma mark - Android-only FD stubs (iOS)

- (void)getContentFd:(NSString *)uri
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  reject(
    @"UNSUPPORTED_PLATFORM",
    @"getContentFd is not supported on iOS",
    nil
  );
}

- (void)closeFd:(NSString *)fdOrPath
        resolve:(RCTPromiseResolveBlock)resolve
         reject:(RCTPromiseRejectBlock)reject
{
  // No-op stub for iOS
  resolve(@(YES));
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeReactNativeLocalDownloadSpecJSI>(params);
}

- (void)persistContentPermission:(NSString *)uri
                          resolve:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject
{
  // iOS sandbox does not support persistable URI permissions
  resolve(@(YES));
}

@end
