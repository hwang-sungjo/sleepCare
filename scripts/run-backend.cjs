/**
 * 레포 루트에서 호출: spring-server Gradle bootRun.
 * bootRun.workingDir 가 레포 루트이므로 루트 `.env` 가 로드됩니다.
 */
const { spawn } = require('child_process')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..')
const springServer = path.join(repoRoot, 'spring-server')
const isWin = process.platform === 'win32'
const gradle = isWin ? 'gradlew.bat' : './gradlew'
const profile = process.env.SPRING_PROFILE || 'local'

const child = spawn(gradle, ['bootRun', `-Pprofile=${profile}`], {
  cwd: springServer,
  stdio: 'inherit',
  shell: isWin,
})

child.on('exit', (code, signal) => {
  if (signal) process.exit(1)
  process.exit(code ?? 0)
})
