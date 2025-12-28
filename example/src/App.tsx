import { Text, View, StyleSheet, TouchableOpacity } from 'react-native';

import { closeFd, getContentFd, localDownload } from '@vali98/react-native-fs';

import * as FS from '@dr.pogodin/react-native-fs';

export default function App() {
  return (
    <View style={styles.container}>
      <TouchableOpacity
        onPress={() => {
          FS.writeFile(FS.DocumentDirectoryPath + '/text.txt', 'testtest', {
            encoding: 'utf8',
          }).then(() => console.log('File Created!'));
        }}
      >
        <Text>Create File</Text>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={() => {
          localDownload(FS.DocumentDirectoryPath + '/text.txt')
            .then(() => console.log('Exported!'))
            .catch((e) => console.log(e));
        }}
      >
        <Text>Export File To Downloads</Text>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={async () => {
          const [pick] = await FS.pickFile({});
          if (!pick) return;

          const fd = await getContentFd(pick);
          console.log(fd);
          if (!fd) return;
          await closeFd(fd);
        }}
      >
        <Text>Get File FD</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
