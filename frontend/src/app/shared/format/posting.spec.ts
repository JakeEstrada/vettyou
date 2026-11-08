import { postingBlocks } from './posting';

describe('postingBlocks', () => {
  it('turns headings, lists, and paragraphs into readable blocks', () => {
    const blocks = postingBlocks(`# Registered Nurse

We are looking for a nurse to join a medical-surgical unit.

Requirements:
- BLS
- ACLS

## About the role
Provide bedside care.`);

    expect(blocks).toEqual([
      { kind: 'heading', text: 'Registered Nurse' },
      { kind: 'paragraph', text: 'We are looking for a nurse to join a medical-surgical unit.' },
      { kind: 'heading', text: 'Requirements' },
      { kind: 'list', items: ['BLS', 'ACLS'] },
      { kind: 'heading', text: 'About the role' },
      { kind: 'paragraph', text: 'Provide bedside care.' },
    ]);
  });
});
