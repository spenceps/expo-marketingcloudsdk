import {ExpoConfig} from '@expo/config-types';

import {NSE_TARGET_NAME} from '../iosConstants';

export default function getEasManagedCredentialsConfigExtra(
  config: ExpoConfig,
  bundleSuffix?: String,
): {
  [k: string]: any;
} {
  return {
    ...config.extra,
    eas: {
      ...config.extra?.eas,
      build: {
        ...config.extra?.eas?.build,
        experimental: {
          ...config.extra?.eas?.build?.experimental,
          ios: {
            ...config.extra?.eas?.build?.experimental?.ios,
            appExtensions: [
              ...(config.extra?.eas?.build?.experimental?.ios?.appExtensions ?? []),
              {
                // keep in sync with native changes in NSE
                targetName: NSE_TARGET_NAME,
                bundleIdentifier: `${config?.ios?.bundleIdentifier}.${
                  bundleSuffix ?? NSE_TARGET_NAME
                }`,
              },
            ],
          },
        },
      },
    },
  };
}
