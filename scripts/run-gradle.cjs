/**
 * 레포 루트에서 spring-server Gradle 임의 태스크 실행.
 * 예: node scripts/run-gradle.cjs build
 */
const { spawn } = require('child_process')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..')
const springServer = path.join(repoRoot, 'spring-server')
const isWin = process.platform === 'win32'
const gradle = isWin ? 'gradlew.bat' : './gradlew'
const args = process.argv.slice(2)

const child = spawn(gradle, args.length ? args : ['tasks', '--all'], {
  cwd: springServer,
  stdio: 'inherit',
  shell: isWin,
})

child.on('exit', (code, signal) => {
  if (signal) process.exit(1)
  process.exit(code ?? 0)
})
