const { SlashCommandBuilder } = require('discord.js');

module.exports = [
    new SlashCommandBuilder()
    .setName('help')
    .setDescription('사용 가능한 명령어를 안내합니다.'),

    new SlashCommandBuilder()
    .setName('hot')
    .setDescription('방송자 실시간 급상승 감지 명령어')
    .addSubcommand(subcommand =>
      subcommand
        .setName('all')
        .setDescription('전체 방송자 실시간 급상승 감지')),
];
