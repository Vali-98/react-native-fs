import ReactNativeLocalDownload from './NativeReactNativeLocalDownload';

import { PermissionsAndroid, Platform, Linking, Alert } from 'react-native';

export async function getContentFd(uri: string) {
  if (Platform.OS !== 'android') return;
  if (!uri.includes('content://')) {
    throw new TypeError('URI provided is not a content URI');
  }
  return ReactNativeLocalDownload.getContentFd(uri);
}

export async function closeFd(uri: string) {
  if (Platform.OS !== 'android') return;
  return ReactNativeLocalDownload.closeFd(uri);
}

export async function persistContentPermission(uri: string) {
  if (Platform.OS !== 'android') return;
  return ReactNativeLocalDownload.persistContentPermission(uri);
}
/**
 *
 * @param uri path to content to be sent to downloads
 * @param never_ask_function callback if android permissions are set to `never_ask_again`
 * @returns
 */
export async function localDownload(
  uri: string,
  never_ask_function?: () => void | undefined
): Promise<void> {
  const granted = await requestStoragePermission();
  if (granted !== 'granted') {
    if (granted === PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) {
      if (!never_ask_function)
        Alert.alert(
          'Permission Required',
          'You have permanently denied storage access. Please enable it from settings.',
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Open Settings', onPress: () => Linking.openSettings() },
          ]
        );
      else never_ask_function();
    }
    throw new Error('Permissions not granted');
  }
  return ReactNativeLocalDownload.localDownload(uri);
}

export const requestStoragePermission = async (): Promise<
  typeof PermissionsAndroid.RESULTS.DENIED
> => {
  if (Platform.OS !== 'android') return PermissionsAndroid.RESULTS.GRANTED;

  try {
    if (Number(Platform.Version) >= 33) {
      return 'granted';
    }

    const hasPermission = await PermissionsAndroid.check(
      PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE
    );
    if (hasPermission) {
      return 'granted';
    }

    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
      {
        title: 'Storage Permission',
        message: 'This app needs access to your storage to download files.',
        buttonPositive: 'OK',
      }
    );
    return granted;
  } catch (err) {
    console.warn(err);
    return PermissionsAndroid.RESULTS.DENIED;
  }
};
