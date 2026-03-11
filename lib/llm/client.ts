import Anthropic from '@anthropic-ai/sdk';

let _client: Anthropic | null = null;

export function isLLMConfigured(): boolean {
  return Boolean(process.env.ANTHROPIC_API_KEY);
}

export function getLLMClient(): Anthropic {
  if (!_client) {
    if (!isLLMConfigured()) {
      throw new Error('ANTHROPIC_API_KEY not configured');
    }
    _client = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });
  }
  return _client;
}

export async function complete(system: string, userMessage: string, maxTokens = 512): Promise<string> {
  const client = getLLMClient();
  const response = await client.messages.create({
    model: 'claude-haiku-4-5-20251001',
    max_tokens: maxTokens,
    system,
    messages: [{ role: 'user', content: userMessage }],
  });
  const block = response.content[0];
  if (block.type !== 'text') throw new Error('Unexpected LLM response type');
  return block.text;
}
