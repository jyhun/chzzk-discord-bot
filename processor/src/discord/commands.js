const { SlashCommandBuilder } = require('discord.js');

module.exports = [
  new SlashCommandBuilder()
    .setName('help')
    .setDescription('사용 가능한 명령어를 안내합니다.'),

  new SlashCommandBuilder()
    .setName('hot')
    .setDescription('실시간 급상승 알림 구독')
    .addStringOption(option =>
      option
        .setName('target')
        .setDescription('all 또는 방송자 채널 ID')
        .setRequired(true)
    )
];
