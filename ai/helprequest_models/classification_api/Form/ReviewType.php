<?php

namespace App\Form;

use App\Entity\Review;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;

class ReviewType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('rating', ChoiceType::class, [
                'label' => 'Rating',
                'choices' => [
                    'Excellent (5)' => 5,
                    'Good (4)' => 4,
                    'Average (3)' => 3,
                    'Below Average (2)' => 2,
                    'Poor (1)' => 1,
                ],
                'expanded' => true,
                'multiple' => false,
                'constraints' => [
                    new NotBlank(['message' => 'Please select a rating']),
                ],
            ])
            ->add('comment', TextareaType::class, [
                'label' => 'Comment (Optional)',
                'required' => false,
                'attr' => [
                    'placeholder' => 'Share your experience...',
                    'rows' => 3,
                    'class' => 'form-control'
                ],
                'constraints' => [
                    new Length(['max' => 5000, 'maxMessage' => 'Comment cannot exceed {{ limit }} characters']),
                ],
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Review::class,
        ]);
    }
}
