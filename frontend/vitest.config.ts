import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

export default defineConfig((env) =>
  mergeConfig(
    viteConfig(env),
    defineConfig({
      test: {
        environment: 'jsdom',
        exclude: [...configDefaults.exclude, 'e2e/**'],
        root: fileURLToPath(new URL('./', import.meta.url)),
        setupFiles: ['./src/test/setup.ts'],
        coverage: {
          provider: 'v8',
          include: ['src/**/*.{ts,vue}'],
          exclude: ['src/**/__tests__/**', 'src/test/**', 'src/main.ts', 'src/**/*.d.ts'],
        },
      },
    }),
  ),
)
