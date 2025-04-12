const { SlashCommandBuilder } = require('discord.js');

module.exports = [
    new SlashCommandBuilder()
    .setName('help')
    .setDescription('사용 가능한 명령어를 안내합니다.'),

    new SlashCommandBuilder()
    .setName('highlight')
    .setDescription('하이라이트 감지 명령어')
    .addSubcommand(subcommand =>
      subcommand
        .setName('all')
        .setDescription('전체 방송자 하이라이트 감지')),
];
