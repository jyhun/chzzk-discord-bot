require('dotenv').config();
const { Client, GatewayIntentBits, REST, Routes, Events, MessageFlags } = require('discord.js');
const commands = require('./commands');
const { collectChatsForHighlight } = require('../crawler/crawler');

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

async function registerCommands() {
  const rest = new REST({ version: '10' }).setToken(process.env.DISCORD_BOT_TOKEN);
  try {
    console.info('[Command Registration] 시작...');
    await rest.put(
      Routes.applicationGuildCommands(
        process.env.DISCORD_CLIENT_ID,
        process.env.DISCORD_GUILD_ID
      ),
      { body: commands.map(cmd => cmd.toJSON()) }
    );
    console.info('[Command Registration] 완료');
  } catch (error) {
    console.error('[Command Registration] 실패:', error);
    throw error;
  }
}

async function startBot() {
  try {
    await registerCommands();
  } catch (error) {
    console.error('[Startup] 등록 중 오류:', error);
  }

  client.once(Events.ClientReady, () => {
    console.info(`[Bot] 로그인 완료: ${client.user.tag}`);
  });

  client.on(Events.InteractionCreate, async interaction => {
    if (!interaction.isCommand()) return;
    const { commandName, options, user } = interaction;

    if (commandName === 'help') {
      try {
        await interaction.reply({
          content: '사용 가능한 명령어:\n' +
                   '/help - 명령어 도움말\n' +
                   '/highlight all - 전체 방송자 하이라이트 감지\n',
          flags: MessageFlags.Ephemeral
        });
      } catch (error) {
        console.error('[Help Command] 오류:', error);
      }
    }

    if (commandName === 'highlight') {
      const subcommand = options.getSubcommand();
      if (subcommand === 'all') {
        console.info(`[Highlight All] ${user.username} (${user.id}) 실행`);
        try {
          await collectChatsForHighlight('all', null);
          await interaction.reply({
            content: '전체 방송자 하이라이트 감지를 시작했습니다!',
            flags: MessageFlags.Ephemeral
          });
        } catch (error) {
          console.error('[Highlight All] 오류:', error);
          await interaction.reply({
            content: '크롤러 실행 중 오류 발생',
            flags: MessageFlags.Ephemeral
          });
        }
      }
    }
  });

  try {
    await client.login(process.env.DISCORD_BOT_TOKEN);
  } catch (error) {
    console.error('[Bot Login] 실패:', error);
  }
}

process.on('unhandledRejection', error => {
  console.error('Unhandled promise rejection:', error);
});

module.exports = startBot;