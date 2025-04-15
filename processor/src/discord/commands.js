const { SlashCommandBuilder } = require('discord.js');

const EVENT_DESC = '이벤트 종류 (HOT, START, END, CHANGE)';
const TARGET_DESC = '방송자 채널 ID';

const commands = [

  // /help
  new SlashCommandBuilder()
    .setName('help')
    .setDescription('사용 가능한 명령어를 안내합니다.'),

  // /subscribe HOT [target]
  new SlashCommandBuilder()
    .setName('subscribe')
    .setDescription('이벤트 구독')
    .addStringOption(option =>
      option.setName('event')
        .setDescription(EVENT_DESC)
        .setRequired(true)
    )
    .addStringOption(option =>
      option.setName('target')
        .setDescription(TARGET_DESC)
        .setRequired(false)
    ),

  // /unsubscribe [event] [target]
  new SlashCommandBuilder()
    .setName('unsubscribe')
    .setDescription('구독 해제')
    .addStringOption(option =>
      option.setName('event')
        .setDescription(EVENT_DESC)
        .setRequired(false)
    )
    .addStringOption(option =>
      option.setName('target')
        .setDescription(TARGET_DESC)
        .setRequired(false)
    ),

  // /subscriptions [event] [target]
  new SlashCommandBuilder()
    .setName('subscriptions')
    .setDescription('구독 목록 조회')
    .addStringOption(option =>
      option.setName('event')
        .setDescription(EVENT_DESC)
        .setRequired(false)
    )
    .addStringOption(option =>
      option.setName('target')
        .setDescription(TARGET_DESC)
        .setRequired(false)
    ),

];

module.exports = commands;
