import {
  animate,
  AnimationTriggerMetadata,
  query,
  stagger,
  style,
  transition,
  trigger,
} from '@angular/animations';

export const fadeInUp: AnimationTriggerMetadata = trigger('fadeInUp', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(16px)' }),
    animate('400ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
  ]),
]);

export const staggerList: AnimationTriggerMetadata = trigger('staggerList', [
  transition('* => *', [
    query(
      ':enter',
      [
        style({ opacity: 0, transform: 'translateY(16px)' }),
        stagger(80, [
          animate('400ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
        ]),
      ],
      { optional: true }
    ),
  ]),
]);
